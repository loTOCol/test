#!/usr/bin/env node
/**
 * Firestore collection migration:
 * matedata -> matedata
 * matedatalist -> matedatalist
 * materequest -> materequest
 * mateuser -> mateuser
 *
 * Usage examples:
 * 1) API key mode (works only if Firestore rules allow):
 *    node scripts/firestore-migrate-mate-to-mate.mjs --project-id walking-465a3 --api-key YOUR_KEY --execute
 *
 * 2) Service account mode (recommended):
 *    node scripts/firestore-migrate-mate-to-mate.mjs --service-account C:\path\service-account.json --execute
 *
 * Default is dry-run; add --execute to actually write.
 */

import fs from "node:fs";
import crypto from "node:crypto";

const PAIRS = [
  ["matedata", "matedata"],
  ["matedatalist", "matedatalist"],
  ["materequest", "materequest"],
  ["mateuser", "mateuser"],
];

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (!a.startsWith("--")) continue;
    const key = a.slice(2);
    const next = argv[i + 1];
    if (!next || next.startsWith("--")) {
      out[key] = true;
    } else {
      out[key] = next;
      i++;
    }
  }
  return out;
}

function b64url(v) {
  return Buffer.from(v).toString("base64url");
}

async function getAccessTokenFromServiceAccount(saPath) {
  const sa = JSON.parse(fs.readFileSync(saPath, "utf8"));
  const iat = Math.floor(Date.now() / 1000);
  const exp = iat + 3600;
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: "https://oauth2.googleapis.com/token",
    iat,
    exp,
  };

  const signingInput = `${b64url(JSON.stringify(header))}.${b64url(JSON.stringify(payload))}`;
  const sign = crypto.createSign("RSA-SHA256");
  sign.update(signingInput);
  sign.end();
  const signature = sign.sign(sa.private_key, "base64url");
  const jwt = `${signingInput}.${signature}`;

  const body = new URLSearchParams({
    grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
    assertion: jwt,
  });

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`Token exchange failed: ${res.status} ${t}`);
  }
  const j = await res.json();
  return { accessToken: j.access_token, projectId: sa.project_id };
}

async function gfetch(url, init = {}) {
  const res = await fetch(url, init);
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`${res.status} ${res.statusText} :: ${text}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

function baseDocUrl(projectId) {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
}

async function listAllDocuments({ projectId, collection, authHeader, apiKey }) {
  const docs = [];
  let pageToken = "";
  const base = `${baseDocUrl(projectId)}/${collection}`;

  while (true) {
    const u = new URL(base);
    u.searchParams.set("pageSize", "300");
    if (pageToken) u.searchParams.set("pageToken", pageToken);
    if (apiKey) u.searchParams.set("key", apiKey);

    const json = await gfetch(u.toString(), {
      headers: authHeader ? { Authorization: authHeader } : {},
    });
    if (Array.isArray(json.documents)) docs.push(...json.documents);
    if (!json.nextPageToken) break;
    pageToken = json.nextPageToken;
  }
  return docs;
}

function getDocId(fullName) {
  const parts = fullName.split("/");
  return parts[parts.length - 1];
}

async function upsertDoc({ projectId, targetCollection, docId, fields, authHeader, apiKey }) {
  const u = new URL(`${baseDocUrl(projectId)}/${targetCollection}/${docId}`);
  if (apiKey) u.searchParams.set("key", apiKey);

  await gfetch(u.toString(), {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      ...(authHeader ? { Authorization: authHeader } : {}),
    },
    body: JSON.stringify({ fields: fields ?? {} }),
  });
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const execute = !!args.execute;
  const saPath = args["service-account"];
  let projectId = args["project-id"];
  const apiKey = args["api-key"];
  let authHeader = "";

  if (saPath) {
    const { accessToken, projectId: saProjectId } = await getAccessTokenFromServiceAccount(saPath);
    authHeader = `Bearer ${accessToken}`;
    projectId = projectId || saProjectId;
  }

  if (!projectId) {
    throw new Error("Missing --project-id (or provide --service-account with project_id).");
  }
  if (!authHeader && !apiKey) {
    throw new Error("Provide either --service-account or --api-key.");
  }

  console.log(`Project: ${projectId}`);
  console.log(`Mode: ${authHeader ? "service-account" : "api-key"}`);
  console.log(`Run: ${execute ? "execute" : "dry-run"}`);

  let totalRead = 0;
  let totalWrite = 0;

  for (const [source, target] of PAIRS) {
    console.log(`\n[${source} -> ${target}] reading...`);
    const docs = await listAllDocuments({ projectId, collection: source, authHeader, apiKey });
    console.log(`[${source} -> ${target}] found ${docs.length} docs`);
    totalRead += docs.length;

    if (!execute) continue;

    for (let i = 0; i < docs.length; i++) {
      const d = docs[i];
      const docId = getDocId(d.name);
      await upsertDoc({
        projectId,
        targetCollection: target,
        docId,
        fields: d.fields,
        authHeader,
        apiKey,
      });
      totalWrite++;
      if ((i + 1) % 100 === 0) {
        console.log(`[${source}] migrated ${i + 1}/${docs.length}`);
      }
    }
    console.log(`[${source} -> ${target}] done: ${docs.length} docs`);
  }

  console.log(`\nSummary: read=${totalRead}, written=${execute ? totalWrite : 0}`);
  if (!execute) {
    console.log("Dry-run complete. Add --execute to apply writes.");
  }
}

main().catch((e) => {
  console.error(`Migration failed: ${e.message}`);
  process.exit(1);
});


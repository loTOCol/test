#!/usr/bin/env node
/**
 * Delete legacy mate collections:
 * - matedata
 * - matedatalist
 * - materequest
 * - mateuser
 *
 * Usage:
 * node scripts/firestore-delete-mate-collections.mjs --project-id walking-465a3 --api-key YOUR_KEY --execute
 *
 * Default is dry-run; add --execute to actually delete.
 */

const SOURCES = ["matedata", "matedatalist", "materequest", "mateuser"];

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (!a.startsWith("--")) continue;
    const k = a.slice(2);
    const n = argv[i + 1];
    if (!n || n.startsWith("--")) {
      out[k] = true;
    } else {
      out[k] = n;
      i++;
    }
  }
  return out;
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

async function listAllDocuments({ projectId, collection, apiKey }) {
  const docs = [];
  let pageToken = "";
  const base = `${baseDocUrl(projectId)}/${collection}`;

  while (true) {
    const u = new URL(base);
    u.searchParams.set("pageSize", "300");
    if (pageToken) u.searchParams.set("pageToken", pageToken);
    u.searchParams.set("key", apiKey);

    const json = await gfetch(u.toString());
    if (Array.isArray(json.documents)) docs.push(...json.documents);
    if (!json.nextPageToken) break;
    pageToken = json.nextPageToken;
  }
  return docs;
}

async function deleteDoc({ docName, apiKey }) {
  const u = new URL(`https://firestore.googleapis.com/v1/${docName}`);
  u.searchParams.set("key", apiKey);
  await gfetch(u.toString(), { method: "DELETE" });
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const execute = !!args.execute;
  const projectId = args["project-id"];
  const apiKey = args["api-key"];

  if (!projectId) throw new Error("Missing --project-id");
  if (!apiKey) throw new Error("Missing --api-key");

  console.log(`Project: ${projectId}`);
  console.log(`Run: ${execute ? "execute" : "dry-run"}`);

  let total = 0;
  let deleted = 0;

  for (const c of SOURCES) {
    console.log(`\n[${c}] reading...`);
    const docs = await listAllDocuments({ projectId, collection: c, apiKey });
    console.log(`[${c}] found ${docs.length} docs`);
    total += docs.length;

    if (!execute) continue;

    for (let i = 0; i < docs.length; i++) {
      await deleteDoc({ docName: docs[i].name, apiKey });
      deleted++;
      if ((i + 1) % 100 === 0) {
        console.log(`[${c}] deleted ${i + 1}/${docs.length}`);
      }
    }
    console.log(`[${c}] deleted ${docs.length} docs`);
  }

  console.log(`\nSummary: found=${total}, deleted=${execute ? deleted : 0}`);
  if (!execute) console.log("Dry-run complete. Add --execute to apply deletes.");
}

main().catch((e) => {
  console.error(`Delete failed: ${e.message}`);
  process.exit(1);
});


package com.example.walkingmate.feature.shop.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.walkingmate.R;
import com.example.walkingmate.feature.shop.data.CoinManager;
import com.example.walkingmate.feature.shop.model.Item;

import java.util.List;

public class ItemAdapter extends BaseAdapter {
    private final Context context;
    private final List<Item> itemList;
    private final LayoutInflater inflater;
    private final boolean isGif;

    public ItemAdapter(Context context, List<Item> itemList, boolean isGif) {
        this.context = context;
        this.itemList = itemList;
        this.inflater = LayoutInflater.from(context);
        this.isGif = isGif;
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.activity_shop_item_detail, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.itemImage = convertView.findViewById(R.id.itemImage);
            viewHolder.itemTitle = convertView.findViewById(R.id.itemTitle);
            viewHolder.itemDescription = convertView.findViewById(R.id.itemDescription);
            viewHolder.itemPrice = convertView.findViewById(R.id.itemPrice);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Item currentItem = (Item) getItem(position);
        if (currentItem == null) {
            return convertView;
        }

        if (isGif) {
            Glide.with(context).asGif().load(currentItem.getImageResId()).into(viewHolder.itemImage);
        } else {
            Glide.with(context).load(currentItem.getImageResId()).into(viewHolder.itemImage);
        }

        if (currentItem.getTitle() == null || currentItem.getTitle().isEmpty()) {
            viewHolder.itemTitle.setText(isGif ? "WALING ANIMAL " + (position + 1) : "ITEM " + (position + 1));
        } else {
            viewHolder.itemTitle.setText(currentItem.getTitle());
        }

        if (currentItem.getDescription() == null || currentItem.getDescription().trim().isEmpty()) {
            viewHolder.itemDescription.setVisibility(View.GONE);
        } else {
            viewHolder.itemDescription.setVisibility(View.VISIBLE);
            viewHolder.itemDescription.setText(currentItem.getDescription());
        }

        if (CoinManager.isGifPurchased(currentItem.getImageResId())) {
            viewHolder.itemPrice.setText("구매 완료");
        } else {
            viewHolder.itemPrice.setText(currentItem.getPrice() + " 코인");
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView itemImage;
        TextView itemTitle;
        TextView itemDescription;
        TextView itemPrice;
    }
}
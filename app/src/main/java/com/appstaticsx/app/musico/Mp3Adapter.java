package com.appstaticsx.app.musico;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class Mp3Adapter extends BaseAdapter {

    private Context context;
    private ArrayList<HashMap<String, Object>> mp3List;
    private LayoutInflater inflater;

    public Mp3Adapter(Context context, ArrayList<HashMap<String, Object>> mp3List) {
        this.context = context;
        this.mp3List = mp3List;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mp3List.size();
    }

    @Override
    public Object getItem(int position) {
        return mp3List.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, parent, false);
        }

        ImageView albumArt = convertView.findViewById(R.id.albumArt);
        TextView name = convertView.findViewById(R.id.mp3Name);
        TextView artist = convertView.findViewById(R.id.mp3Artist);
        TextView duration = convertView.findViewById(R.id.mp3Duration);
        TextView size = convertView.findViewById(R.id.mp3Size);

        HashMap<String, Object> mp3Info = mp3List.get(position);

        albumArt.setImageBitmap((Bitmap) mp3Info.get("albumArt"));
        name.setText((String) mp3Info.get("name"));
        artist.setText((String) mp3Info.get("artist"));
        duration.setText((String) mp3Info.get("duration"));
        size.setText((String) mp3Info.get("size"));

        return convertView;
    }
}



package com.simple.net.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import org.simple.imageloader.core.SimpleImageLoader;

public class ImagesFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gridview_fragment, container,
                false);
        GridView gridView = (GridView) view.findViewById(R.id.id_gridview);
        gridView.setAdapter(new ImageItemAdaper(getActivity(), 0,
                MainActivity.imageThumbUrls));
        return view;
    }

    /**
     * @author mrsimple
     */
    private class ImageItemAdaper extends ArrayAdapter<String> {

        public ImageItemAdaper(Context context, int resource, String[] datas) {
            super(getActivity(), 0, datas);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(
                        R.layout.image_item_layout, parent, false);
            }
            ImageView imageview = (ImageView) convertView
                    .findViewById(R.id.id_img);
            // 加载图片
            SimpleImageLoader.getInstance().displayImage(imageview,
                    getItem(position));
            return convertView;
        }

    } // end ImageItemAdaper

}

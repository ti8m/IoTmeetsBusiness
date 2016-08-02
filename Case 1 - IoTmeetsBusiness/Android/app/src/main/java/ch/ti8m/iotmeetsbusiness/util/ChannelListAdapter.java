package ch.ti8m.iotmeetsbusiness.util;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.persistency.DataChannel;

/**
 * Connecting channel data with listview-items
 */
public class ChannelListAdapter extends ArrayAdapter<DataChannel> {

    Context context;
    int layoutResourceId;
    ArrayList<DataChannel> data = null;

    public ChannelListAdapter(Context context, int layoutResourceId, ArrayList<DataChannel> channelList) {
        super(context, layoutResourceId, channelList);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = channelList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ChannelHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ChannelHolder();
            holder.icon = (ImageView) row.findViewById(R.id.ic_channelIcon);
            holder.name = (TextView) row.findViewById(R.id.txt_name);
            holder.location = (TextView) row.findViewById(R.id.txt_Location);
            holder.value1 = (TextView) row.findViewById(R.id.txt_value1);
            holder.value2 = (TextView) row.findViewById(R.id.txt_value2);

            row.setTag(holder);
        } else {
            holder = (ChannelHolder) row.getTag();
        }

        // Selected channel
        DataChannel channel = data.get(position);


        // Set icon
        holder.icon.setImageResource(R.mipmap.ic_sensor);

        // Set name
        holder.name.setText(channel.getDescription());

        // Set location
        String location = channel.getLocation();

        if(location != null && !location.equals("")){
            holder.location.setText(location);
        }
        else {
            holder.location.setText(location + "");
            holder.location.setPadding(0,0,0,0);
        }

        // Set value 1
        holder.value1.setText(channel.getValue1());

        // Set value 2
        holder.value2.setText(channel.getValue2());

        return row;
    }


    static class ChannelHolder {
        ImageView icon;
        TextView name;
        TextView location;
        TextView value1;
        TextView value2;

    }
}
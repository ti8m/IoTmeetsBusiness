package ch.ti8m.iotmeetsbusiness.util;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import ch.ti8m.iotmeetsbusiness.R;
import ch.ti8m.iotmeetsbusiness.activities.ConnectPhoneActivity;
import ch.ti8m.iotmeetsbusiness.persistency.DataChannel;

/**
 * Fragment for the listview in home-activity
 */
public class MyListFragment extends Fragment {

    private ListView listView;
    ArrayList<DataChannel> channels;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_fragment, container, false);

        listView = (ListView) view.findViewById(R.id.listFragment);

        //On Click-Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getActivity(), ConnectPhoneActivity.class);
                String writeKey = channels.get(position).getWriteKey();
                intent.putExtra("WRITE_KEY", writeKey);
                startActivity(intent);
            }
        });

        return view;
    }


    /**
     * Set all channels in the listview
     *
     * @param channels
     */
    public void setChannelsOnList(ArrayList<DataChannel> channels) {

        this.channels = channels;
        ChannelListAdapter channelAdapter = new ChannelListAdapter(this.getActivity(), R.layout.list_item, this.channels);
        listView.setAdapter(channelAdapter);
    }

}
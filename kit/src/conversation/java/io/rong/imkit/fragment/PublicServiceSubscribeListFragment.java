package io.rong.imkit.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.sea_monster.resource.Resource;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.cache.RongCache;
import io.rong.imkit.model.Event;
import io.rong.imkit.widget.ArraysDialogFragment;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.PublicServiceProfileList;

/**
 * Created by zhjchen on 4/19/15.
 */

public class PublicServiceSubscribeListFragment extends DispatchResultFragment {

    private ListView mListView;
    private PublicServiceListAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_public_service_sub_list, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mListView = (ListView) view.findViewById(R.id.rc_list);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PublicServiceProfile info = mAdapter.getItem(position);

                RongIM.getInstance().startConversation(getActivity(), info.getConversationType(), info.getTargetId(), info.getName());
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                String[] item = new String[1];
                final PublicServiceProfile info = mAdapter.getItem(position);
                if(info.getConversationType() == Conversation.ConversationType.PUBLIC_SERVICE)
                    item[0] = getActivity().getString(R.string.rc_pub_service_info_unfollow);
                ArraysDialogFragment.newInstance(info.getName(), item).setArraysDialogItemListener(
                        new ArraysDialogFragment.OnArraysDialogItemListener() {
                            @Override
                            public void OnArraysDialogItemClick(DialogInterface dialog, int which) {
                                Conversation.PublicServiceType publicServiceType = null;
                                if(info.getConversationType() == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                                    publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                                else if(info.getConversationType() == Conversation.ConversationType.PUBLIC_SERVICE)
                                    publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                                else
                                    System.err.print("the public service type is error!!");

                                RongIMClient.getInstance().unsubscribePublicService(publicServiceType, info.getTargetId(), new RongIMClient.OperationCallback() {
                                    @Override
                                    public void onSuccess() {
                                        mAdapter.remove(position);
                                        mAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onError(RongIMClient.ErrorCode errorCode) {

                                    }
                                });
                            }
                        }).show(getFragmentManager());
                return true;
            }
        });

        mAdapter = new PublicServiceListAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        getDBData();

        RongContext.getInstance().getEventBus().register(this);
    }


    private void getDBData() {

        if(RongIM.getInstance()!=null && RongIM.getInstance().getRongIMClient()!=null) {
            RongIM.getInstance().getRongIMClient().getPublicServiceList(new RongIMClient.ResultCallback<PublicServiceProfileList>() {
                @Override
                public void onSuccess(PublicServiceProfileList infoList) {
                    RongCache<String, PublicServiceProfile> cache = RongContext.getInstance().getPublicServiceInfoCache();
                    String separator = "#@6RONG_CLOUD9@#";

                    for (PublicServiceProfile info : infoList.getPublicServiceData()) {
                        String key = info.getTargetId() + separator + info.getConversationType().getValue();
                        if (cache.get(key) == null)
                            cache.put(key, info);
                    }

                    mAdapter.clear();
                    mAdapter.addCollection(infoList.getPublicServiceData());
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    @Override
    protected void initFragment(Uri uri) {

    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    private class PublicServiceListAdapter extends io.rong.imkit.widget.adapter.BaseAdapter<PublicServiceProfile> {

        LayoutInflater mInflater;

        public PublicServiceListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        protected View newView(Context context, int position, ViewGroup group) {
            View view = mInflater.inflate(R.layout.rc_item_public_service_list, null);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.portrait = (AsyncImageView) view.findViewById(R.id.portrait);
            viewHolder.name = (TextView) view.findViewById(R.id.name);
            viewHolder.introduction = (TextView) view.findViewById(R.id.introduction);
            view.setTag(viewHolder);

            return view;
        }

        @Override
        protected void bindView(View v, int position, PublicServiceProfile data) {
            ViewHolder viewHolder = (ViewHolder) v.getTag();

            if (data != null) {
                viewHolder.portrait.setResource(new Resource(data.getPortraitUri()));
                viewHolder.name.setText(data.getName());
                viewHolder.introduction.setText(data.getIntroduction());
            }
        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public PublicServiceProfile getItem(int position) {
            return super.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }


        class ViewHolder {
            AsyncImageView portrait;
            TextView name;
            TextView introduction;
        }
    }

    public void onEvent(Event.PublicServiceFollowableEvent event) {
        if (event != null) {
            getDBData();
        }
    }


    @Override
    public void onDestroyView() {
        RongContext.getInstance().getEventBus().unregister(this);
        super.onDestroyView();
    }
}

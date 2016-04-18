package io.rong.imkit.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.Event;
import io.rong.imkit.widget.LoadingDialogFragment;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.PublicServiceProfileList;
import com.sea_monster.resource.Resource;
import io.rong.imkit.widget.AsyncImageView;

/**
 * Created by zhjchen on 4/19/15.
 */

public class PublicServiceSearchFragment extends DispatchResultFragment {

    private EditText mEditText;
    private Button mSearchBtn;
    private ListView mListView;
    private PublicServiceListAdapter mAdapter;

    LoadingDialogFragment mLoadingDialogFragment;

    @Override
    protected void initFragment(Uri uri) {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.rc_fr_public_service_search, container, false);

        mEditText = (EditText) view.findViewById(R.id.rc_search_ed);
        mSearchBtn = (Button) view.findViewById(R.id.rc_search_btn);
        mListView = (ListView) view.findViewById(R.id.rc_search_list);
        RongContext.getInstance().getEventBus().register(this);

        return view;
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLoadingDialogFragment = LoadingDialogFragment.newInstance("", getResources().getString(R.string.rc_notice_data_is_loading));

        mSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoadingDialogFragment.show(getFragmentManager());

                if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
                    RongIM.getInstance().getRongIMClient().searchPublicService(RongIMClient.SearchType.EXACT, mEditText.getText().toString(), new RongIMClient.ResultCallback<PublicServiceProfileList>() {

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {
                            mLoadingDialogFragment.dismiss();
                        }

                        @Override
                        public void onSuccess(PublicServiceProfileList list) {
                            mAdapter.clear();
                            mAdapter.addCollection(list.getPublicServiceData());
                            mAdapter.notifyDataSetChanged();
                            mLoadingDialogFragment.dismiss();
                        }
                    });
                }
            }
        });

        mAdapter = new PublicServiceListAdapter(PublicServiceSearchFragment.this.getActivity());
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PublicServiceProfile info = mAdapter.getItem(position);

                if(info.isFollow()) {
                    RongIM.getInstance().startConversation(getActivity(), info.getConversationType(), info.getTargetId(), info.getName());
                } else {
                    Uri uri = Uri.parse("rong://" + view.getContext().getApplicationInfo().packageName).buildUpon()
                            .appendPath("publicServiceProfile").appendPath(info.getConversationType().getName().toLowerCase()).appendQueryParameter("targetId", info.getTargetId()).build();

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                    intent.putExtra(PublicServiceProfileFragment.AGS_PUBLIC_ACCOUNT_INFO, info);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        RongContext.getInstance().getEventBus().unregister(this);
        super.onDestroy();
    }

    private class PublicServiceListAdapter extends io.rong.imkit.widget.adapter.BaseAdapter<PublicServiceProfile> {

        LayoutInflater mInflater;

        public PublicServiceListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        protected View newView(Context context, int position, ViewGroup group) {
            View view = mInflater.inflate(R.layout.rc_item_public_service_search, null);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.portrait = (AsyncImageView) view.findViewById(R.id.portrait);
            viewHolder.name = (TextView) view.findViewById(R.id.name);
            view.setTag(viewHolder);

            return view;
        }

        @Override
        protected void bindView(View v, int position, PublicServiceProfile data) {
            ViewHolder viewHolder = (ViewHolder) v.getTag();

            if (data != null) {
                viewHolder.portrait.setResource(new Resource(data.getPortraitUri()));
                viewHolder.name.setText(data.getName());
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
        }
    }

    public void onEventMainThread(Event.PublicServiceFollowableEvent event) {
        RLog.d(this, "onEventMainThread", "PublicAccountIsFollowEvent, follow=" + event.isFollow());
        if(event != null) {
            getActivity().finish();
        }
    }
}

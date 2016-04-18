package io.rong.app.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.rong.app.R;
import io.rong.imkit.RongIM;
import io.rong.imlib.model.Conversation;

/**
 * Created by Administrator on 2015/3/6.
 */
public class CustomerFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.de_fr_customer, container, false);

        TextView mCustomerChat = (TextView) view.findViewById(R.id.customer_chat);
        mCustomerChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RongIM.getInstance().startConversation(getActivity(), Conversation.ConversationType.APP_PUBLIC_SERVICE, "KEFU144542424649464", "在线客服");
            }
        });

        return view;
    }

}

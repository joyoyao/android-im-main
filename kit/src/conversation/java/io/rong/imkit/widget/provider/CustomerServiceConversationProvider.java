package io.rong.imkit.widget.provider;

import io.rong.imkit.model.ConversationProviderTag;
import io.rong.imkit.model.UIConversation;

@ConversationProviderTag(conversationType = "customer_service", portraitPosition = 1)
public class CustomerServiceConversationProvider extends PrivateConversationProvider implements IContainerItemProvider.ConversationProvider<UIConversation>{


}

package io.rong.imkit.common;

/**
 * Created by DragonJ on 14-8-1.
 */
public class RongConst {
    public static final String RES_DIC = "rong_cloud";
    public static final int WORK_QUEUE_MAX_COUNT = 30;
    public static final int DEF_THREAD_WORKER_COUNT = 2;
    public static final int MAX_THREAD_WORKER_COUNT = 3;
    public static final int CREATE_THREAD_TIME_SPAN = 3;

    public static class API {
        public static String HOST = "";
    }

    public static class Cache{
        public static final int USER_CACHE_MAX_COUNT = 100;
        public static final int PUBLIC_ACCOUNT_CACHE_MAX_COUNT = 50;
        public static final int GROUP_CACHE_MAX_COUNT = 10;
        public static final int DISCUSSION_CACHE_MAX_COUNT = 10;
        public static final int NOTIFICATION_CACHE_MAX_COUNT = 10;
    }

    public static class EXTRA{
        public static final String CONTENT = "extra_fragment_content";
        public static final String USERS = "extra_users";
        public static final String USER = "extra_user";
        public static final String CONVERSATION= "extra_conversation";
        public static final String KEY = "extra_key";
        public static final String RES = "extra_res";
    }
}

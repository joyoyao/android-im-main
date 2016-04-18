package io.rong.imkit.cache;

import io.rong.imkit.RongContext;
import com.sea_monster.common.PriorityRunnable;

/**
 * Created by DragonJ on 14/12/12.
 */
public abstract class RongCacheWrap<K, V> extends RongCache<K, V> {

    RongContext mContext;
    boolean mIsSync = false;

    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */
    public RongCacheWrap(RongContext context, int maxSize) {
        super(maxSize);
        mContext = context;
    }

    public boolean isIsSync() {
        return mIsSync;
    }

    public void setIsSync(boolean isSync) {
        this.mIsSync = isSync;
    }

    @Override
    protected V create(K key) {
        if(key == null)
            return null;
        if (!mIsSync)
            executeCacheProvider(key);
        else
            return obtainValue(key);

        return super.create(key);
    }

    protected RongContext getContext() {
        return mContext;
    }

    public void executeCacheProvider(final K key) {
        mContext.executorBackground(new PriorityRunnable(PriorityRunnable.HIGH) {
            @Override
            public void run() {
                obtainValue(key);
            }
        });
    }

    public abstract V obtainValue(K key);


}

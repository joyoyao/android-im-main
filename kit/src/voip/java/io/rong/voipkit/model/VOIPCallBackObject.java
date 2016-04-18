package io.rong.voipkit.model;

/**
 * Created by zhjchen on 1/9/15.
 */

public class VOIPCallBackObject {

    private boolean isSuccess;
    private String objectName;
    private Object object;

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}

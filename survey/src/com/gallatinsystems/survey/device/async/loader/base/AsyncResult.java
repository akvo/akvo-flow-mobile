package com.gallatinsystems.survey.device.async.loader.base;

public class AsyncResult<T> {
    
    protected Exception mException;
    protected T mData;
    
    public AsyncResult() {}
    
    public AsyncResult (T data, Exception exception) {
        this.mData = data;
        this.mException = exception;
    }
    
    public AsyncResult (T data, String error) {
        this.mData = data;
        this.mException = error != null ? new Exception(error) : null;
    }

    public void setException(Exception exception) {
        this.mException = exception;
    }

    public Exception getException() {
        return mException;
    }

    public void setData(T data) {
        this.mData = data;
    }

    public T getData() {
        return mData;
    }
}

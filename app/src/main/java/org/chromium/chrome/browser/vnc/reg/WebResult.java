package org.chromium.chrome.browser.vnc.reg;

public class WebResult {
    private boolean status;//true、成功 false、失败
    private Object result;

    public WebResult() {
        super();
        this.status = true;
    }

    public WebResult(Object result) {
        super();
        this.status = true;
        this.result = result;
    }

    /**
     * @return the status
     */
    public boolean isStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(boolean status) {
        this.status = status;
    }

    /**
     * @return the result
     */
    public Object getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "Result [status=" + status + ", result=" + result + "]";
    }
}

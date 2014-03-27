package com.android.fancyblurdemo.app.imageblur;

/**
 * Created by kevin.marlow on 3/26/14.
 */
public class BlurError extends Exception {

    public BlurError() {}

    public BlurError(String exceptionMessage) {
        super(exceptionMessage);
    }

    public BlurError(String exceptionMessage, Throwable reason) {
        super(exceptionMessage, reason);
    }

    public BlurError(Throwable cause) {
        super(cause);
    }
}

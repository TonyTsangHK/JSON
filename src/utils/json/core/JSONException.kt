package utils.json.core

/**
 * The JSONException is thrown by the JSON.org classes when things are amiss.
 * 
 * Converted from https://github.com/stleary/JSON-java/blob/master/JSONException.java
 * 
 * @author JSON.org
 * @version 2015-12-09
 */
class JSONException : RuntimeException {
    /**
     * Constructs a JSONException with an explanatory message.
     *
     * @param message Detail about the reason for the exception.
     */
    constructor(message: String) : super(message)

    /**
     * Constructs a new JSONException with the specified cause.
     *
     * @param cause The cause.
     */
    constructor(cause: Throwable) : super(cause.message, cause)

    /**
     * Constructs a JSONException with an explanatory message and cause.
     *
     * @param message Detail about the reason for the exception.
     * @param cause The cause.
     */
    constructor(message: String, cause: Throwable): super(message, cause)
}

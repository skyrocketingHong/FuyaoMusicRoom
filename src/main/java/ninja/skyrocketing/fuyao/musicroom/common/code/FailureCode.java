package ninja.skyrocketing.fuyao.musicroom.common.code;

/**
 * @author skyrocketing Hong
 */

public enum FailureCode implements Code {

    /**
     * default
     */
    FAILURE("40000", "failure");

    private final String code;
    private final String message;

    FailureCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return this.code;
    }

    @Override
    public String message() {
        return this.message;
    }
}

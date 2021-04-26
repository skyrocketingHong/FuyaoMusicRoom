package ninja.skyrocketing.fuyao.musicroom.common.code;

/**
 * @author skyrocketing Hong
 */
public enum SuccessCode implements Code {

    /**
     * default ...
     */
    SUCCESS("20000", "success");

    private final String code;
    private final String message;

    SuccessCode(String code, String message) {
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

package dev.zeann3th.file.exception;

import java.util.Map;

public class CommandExceptionBuilder {
    public static CommandException exception(ErrorCode errorCode, Map<String, Object> params) {
        throw new CommandException(errorCode, params);
    }

    public static CommandException exception(ErrorCode errorCode) {
        throw new CommandException(errorCode);
    }

    private CommandExceptionBuilder() {
    }
}

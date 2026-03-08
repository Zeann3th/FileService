package dev.zeann3th.file.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    FS0000(ErrorType.SUCCESS, "Success"),
    FS0001(ErrorType.FAILURE, "File not found: <key>"),
    FS0002(ErrorType.FAILURE, "Invalid filename: <reason>"),
    FS0003(ErrorType.FAILURE, "Error generating presigned URL: <reason>"),
    FS0004(ErrorType.FAILURE, "Error retrieving file: <reason>"),
    FS9999(ErrorType.FAILURE, "System is busy, please try again later"),;

    private final ErrorType errorType;
    private final String message;

    ErrorCode(ErrorType errorType, String message) {
        this.errorType = errorType;
        this.message = message;
    }
}

package boat.carpetorgaddition.exception;

public class TaskExecutionException extends RuntimeException {
    /**
     * 异常的应对措施
     */
    private final Runnable handler;

    public TaskExecutionException(Runnable handler) {
        this.handler = handler;
    }

    /**
     * 处理异常
     */
    public void disposal() {
        this.handler.run();
    }
}

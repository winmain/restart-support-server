package server;

public class Config {
    public String listenHost = "127.0.0.1";
    public int listenPort = 0;

    public String serverHost = "127.0.0.1";
    public int serverPort = 0;

    /**
     * Период проверки работоспособности main сервера, когда он уходит в офлайн.
     */
    public long mainServerCheckPeriodMillis = 500;

    /**
     * Максимальное количество попыток (одна попытка в секунду) передачи данных main серверу при закрытии, если ещё есть необработанные запросы.
     */
    public int maxRetriesBeforeStop = 5;


    public Config(String[] args) {
        for (String arg : args) {
            String[] split = arg.split("=", 2);
            String param = split[0];
            String value = split[1];

            if (param.equals("--listen-host")) listenHost = value;
            else if (param.equals("--listen-port")) listenPort = Integer.parseInt(value);
            else if (param.equals("--server-host")) serverHost = value;
            else if (param.equals("--server-port")) serverPort = Integer.parseInt(value);
            else if (param.equals("--check-period")) mainServerCheckPeriodMillis = Long.parseLong(value);
            else if (param.equals("--before-stop")) maxRetriesBeforeStop = Integer.parseInt(value);
            else {
                throw new IllegalArgumentException("Unknown argument " + param);
            }
        }

        if (listenPort == 0) throw new RuntimeException("Param --listen-port not defined");
        if (serverPort == 0) throw new RuntimeException("Param --server-port not defined");
    }
}

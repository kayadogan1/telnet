package service.dynamicCalculation;

public interface IDataFetcher {
    void connect();       // Veri kaynağına bağlanma
    void disconnect();    // Bağlantıyı sonlandırma
    void subscribe(String rateName);   // Bir kura abone olma
    void unsubscribe(String rateName); // Abonelikten çıkma
}

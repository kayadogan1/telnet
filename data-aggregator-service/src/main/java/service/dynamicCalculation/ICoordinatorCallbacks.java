package service.dynamicCalculation;

import model.Rate;
import model.RateFields;
import model.RateStatus;

public interface ICoordinatorCallbacks {

    // Bağlantı başarılı olduğunda tetiklenen callback
    void onConnect(String platformName, boolean status);

    // Bağlantı kesildiğinde tetiklenen callback
    void onDisconnect(String platformName, boolean status);

    // İlk defa bir rate verisi geldiğinde tetiklenen callback
    void onRateAvailable(String platformName, String rateName, Rate rate);

    // Rate verisi güncellendiğinde tetiklenen callback
    void onRateUpdate(String platformName, String rateName, RateFields rateFields);

    // Rate verisinin durumu değiştiğinde tetiklenen callback
    void onRateStatus(String platformName, String rateName, RateStatus rateStatus);
}


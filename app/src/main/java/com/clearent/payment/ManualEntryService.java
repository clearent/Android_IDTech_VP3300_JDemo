package com.clearent.payment;

import com.clearent.Constants;
import com.clearent.idtech.android.family.HasManualTokenizingSupport;
import com.clearent.idtech.android.token.manual.ManualCardTokenizer;
import com.clearent.idtech.android.token.manual.ManualCardTokenizerImpl;
import com.clearent.idtech.android.token.manual.ManualEntry;

public class ManualEntryService {

    private static ManualEntryService manualEntryService;
    private ManualCardTokenizer manualCardTokenizer;

    private ManualEntryService(HasManualTokenizingSupport hasManualTokenizingSupport) {
        manualCardTokenizer = new ManualCardTokenizerImpl(hasManualTokenizingSupport);
    }

    public static ManualEntryService getInstance(HasManualTokenizingSupport hasManualTokenizingSupport) {
        if (manualEntryService == null) {
            manualEntryService = new ManualEntryService(hasManualTokenizingSupport);
        }
        return manualEntryService;
    }


    public void createTransactionToken(ManualEntry manualEntry) {
        manualCardTokenizer.createTransactionToken(manualEntry);
    }

    public CreditCard createCreditCard(String cardNumber, String expirationDate, String csc) {
        CreditCard creditCard = new CreditCard();
        creditCard.setCard(cardNumber);
        creditCard.setExpirationDateMMYY(expirationDate);
        creditCard.setCsc(csc);
        creditCard.setSoftwareType(Constants.SOFTWARE_TYPE);
        creditCard.setSoftwareTypeVersion(Constants.SOFTWARE_TYPE_VERSION);
        return creditCard;
    }
}

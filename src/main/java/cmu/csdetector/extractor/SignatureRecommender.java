package cmu.csdetector.extractor;

public class SignatureRecommender {

    private ExtractedMethod em;

    public SignatureRecommender(ExtractedMethod em) {
        this.em = em;
    }

    private String[] recommendParameters(boolean isFeatureEnvy) {
        return new String[]{""};

    }

    private String[] recommendReturnTypes() {
        return new String[]{""};
    }
}

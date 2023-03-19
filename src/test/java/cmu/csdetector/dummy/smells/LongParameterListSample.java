package cmu.csdetector.dummy.smells;

public class LongParameterListSample {
    // Has smell
    public void LongParameterListSampleMethod(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t, int u, int v, int w, int x, int y, int z) {
        System.out.println("LongParameterListSampleMethod");
    }

    // Has smell because the average number of parameters is about 1.1
    public void FourParameterListSampleMethod(double a, String b, int c, byte d) {
        System.out.println("ThreeParameterListSampleMethod");
    }

    // No smell because 2 parameters is not greater than 3
    public void TwoParameterListSampleMethod(float a, boolean b) {
        System.out.println("TwoParameterListSampleMethod");
    }
}

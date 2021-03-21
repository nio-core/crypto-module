import org.junit.Test;

public class Rndm {

    @Test
    public void test() {
        String s = "3447, 1119, 2859, 1851, 894, 972, 1058, 1632, 1199, 2904, 1232, 2399, 1494, 2170, 2120, 2140, 1489, 2156, 2827, 1698, 2730, 2433, 2108, 2253, 3217, 3697, 2139, 3779, 4842, 2781, 3304, 3981, 1802, 4367, 2461, 2460, 3037, 5495, 5832, 5325, 5323, 6245, 6237, 5930, 5549, 5882, 6282, 5382, 5584, 6063, 6178, 5421, 5443, 6280, 6158, 6222, 5844, 6203, 6246, 5749, 5370, 6059, 5303, 5991, 6312, 5999, 5568, 6048, 6193";

        s = s.trim();
        s = s.replace(",", "\n");
        s = s.replace(" ", "");
        System.out.println(s);
    }

    @Test
    public void test2() {
        String s = "5290, 3860, 3972, 3661, 3846, 4947, 3311, 5679, 4676, 3340, 5134, 7140";
        s = s.replace(",", "");
        String arr[] = s.split(" ");
        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            count+= Integer.parseInt(arr[i]);
        }
        double avg = count / arr.length;
        System.out.println("avg: " + avg);
    }
}

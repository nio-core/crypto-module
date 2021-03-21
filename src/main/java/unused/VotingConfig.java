package unused;

import com.google.gson.Gson;

public class VotingConfig {

    String evaluator = null, sender = null, process = null, strategy = null;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}

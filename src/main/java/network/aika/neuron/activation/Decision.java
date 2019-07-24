package network.aika.neuron.activation;


public enum Decision {
    SELECTED('S'),
    EXCLUDED('E'),
    UNKNOWN('U');

    char s;

    Decision(char s) {
        this.s = s;
    }


    Decision getInverted() {
        switch(this) {
            case SELECTED:
                return EXCLUDED;
            case EXCLUDED:
                return SELECTED;
            default:
                return UNKNOWN;
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mammar
 */
public class HMM_Model {

    public static enum Attesation_States {
        ONE, ZERO, UN
    };

    public static enum Transition_States {
        ONE_ONE, ONE_ZERO, ZERO_ZERO, ZERO_ONE
    };

    public static enum Initial_States {
        ONE, ZERO
    };

    public Attesation_States last_Attest_state = Attesation_States.UN;

    public HashMap<Transition_States, Integer> objtrasnitionProbabliteis = new HashMap<Transition_States, Integer>();
    public HashMap<Initial_States, Integer> objInitialProbablites = new HashMap<Initial_States, Integer>();
    public List<Feature> lstFeatures = new ArrayList<Feature>();

    public HMM_Model() {
        last_Attest_state = Attesation_States.UN;

        for (Transition_States ii : Transition_States.values()) {
            objtrasnitionProbabliteis.put(ii, 1);
        }

        for (Initial_States ii : Initial_States.values()) {
            objInitialProbablites.put(ii, 1);
        }

    }

    public void UpdateModel(Attesation_States lastAttest, Attesation_States objNewValue, Map<Feature.Features, Integer> mapObservations, int interation_num, int Num_Iterations_per_sequence) {

        if (lastAttest == Attesation_States.ONE && objNewValue == Attesation_States.ONE) {
            objtrasnitionProbabliteis.put(Transition_States.ONE_ONE, objtrasnitionProbabliteis.get(Transition_States.ONE_ONE) + 1);
        } else if (lastAttest == Attesation_States.ONE && objNewValue == Attesation_States.ZERO) {
            objtrasnitionProbabliteis.put(Transition_States.ONE_ZERO, objtrasnitionProbabliteis.get(Transition_States.ONE_ZERO) + 1);
        } else if (lastAttest == Attesation_States.ZERO && objNewValue == Attesation_States.ONE) {
            objtrasnitionProbabliteis.put(Transition_States.ZERO_ONE, objtrasnitionProbabliteis.get(Transition_States.ZERO_ONE) + 1);
        } else if (lastAttest == Attesation_States.ZERO && objNewValue == Attesation_States.ZERO) {
            objtrasnitionProbabliteis.put(Transition_States.ZERO_ZERO, objtrasnitionProbabliteis.get(Transition_States.ZERO_ZERO) + 1);
        }

        if (interation_num % Num_Iterations_per_sequence == 0) {
            if (objNewValue == Attesation_States.ONE) {
                objInitialProbablites.put(Initial_States.ONE, objInitialProbablites.get(Initial_States.ONE) + 1);
            }
            if (objNewValue == Attesation_States.ZERO) {
                objInitialProbablites.put(Initial_States.ZERO, objInitialProbablites.get(Initial_States.ZERO) + 1);
            }
        }

        if (objNewValue == Attesation_States.ONE) {
            for (Feature objFea : lstFeatures) {
                objFea.updateOneDistribution(mapObservations.get(objFea.FeatureName));
            }
        }

        if (objNewValue == Attesation_States.ZERO) {
            for (Feature objFea : lstFeatures) {
                objFea.updateZeroDistribution(mapObservations.get(objFea.FeatureName));
            }
        }

    }

    public int probablityBeingSucessAttessted(Map<Feature.Features, Integer> mapObservations) {
        double prod_observation_one = 1;
        double prod_observation_zero = 1;

        for (Feature objBE : lstFeatures) {
            prod_observation_one = prod_observation_one * objBE.OneValDistribution.get(mapObservations.get(objBE.FeatureName));
        }

        for (Feature objBE : lstFeatures) {
            prod_observation_zero = prod_observation_zero * objBE.ZeroValDistribution.get(mapObservations.get(objBE.FeatureName));
        }

        double one_prop = prod_observation_one * (objtrasnitionProbabliteis.get(Transition_States.ONE_ONE) * objInitialProbablites.get(Initial_States.ONE)
                + objtrasnitionProbabliteis.get(Transition_States.ZERO_ONE) * objInitialProbablites.get(Initial_States.ZERO));

        double one_zero = prod_observation_zero * (objtrasnitionProbabliteis.get(Transition_States.ZERO_ZERO) * objInitialProbablites.get(Initial_States.ZERO)
                + objtrasnitionProbabliteis.get(Transition_States.ONE_ZERO) * objInitialProbablites.get(Initial_States.ONE));

        return (int) ((one_prop / (one_prop + one_zero)) * 100);

    }

}

/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis2.deployment.util;

import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.phaseresolver.PhaseMetadata;
import org.apache.axis2.phaseresolver.PhaseException;
import org.apache.axis2.om.OMElement;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;

public class PhasesInfo {

    private ArrayList INPhases;
    private ArrayList OUTPhases;
    private ArrayList IN_FaultPhases;
    private ArrayList OUT_FaultPhases;

    public void setINPhases(ArrayList INPhases) {
        this.INPhases = INPhases;
    }

    public void setOUTPhases(ArrayList OUTPhases) {
        this.OUTPhases = OUTPhases;
    }

    public void setIN_FaultPhases(ArrayList IN_FaultPhases) {
        this.IN_FaultPhases = IN_FaultPhases;
    }

    public void setOUT_FaultPhases(ArrayList OUT_FaultPhases) {
        this.OUT_FaultPhases = OUT_FaultPhases;
    }

    public ArrayList getINPhases() {
        return INPhases;
    }

    public ArrayList getOUTPhases() {
        return OUTPhases;
    }

    public ArrayList getIN_FaultPhases() {
        return IN_FaultPhases;
    }

    public ArrayList getOUT_FaultPhases() {
        return OUT_FaultPhases;
    }

    public ArrayList getOperationInPhases() {
        ArrayList operationINPhases = new ArrayList();
        operationINPhases.add(new Phase(PhaseMetadata.PHASE_POLICY_DETERMINATION));
        for (int i = 0; i < INPhases.size(); i++) {
            String phaseName = (String) INPhases.get(i);
            if (PhaseMetadata.PHASE_TRANSPORTIN.equals(phaseName) ||
                    PhaseMetadata.PHASE_PRE_DISPATCH.equals(phaseName) ||
                    PhaseMetadata.PHASE_DISPATCH.equals(phaseName) ||
                    PhaseMetadata.PHASE_POST_DISPATCH.equals(phaseName)) {
            } else {
                operationINPhases.add(new Phase(phaseName));
            }
        }
        return operationINPhases;
    }

    public Phase makePhase(OMElement phaseElement) throws PhaseException {
        String phaseName = phaseElement.getAttributeValue(new QName("name"));
        Phase phase = new Phase(phaseName);
        Iterator children = phaseElement.getChildElements();
        while (children.hasNext()) {
            OMElement handlerElement = (OMElement) children.next();
            HandlerDescription handlerDesc = makeHandler(handlerElement);
            phase.addHandler(handlerDesc);
        }
        return phase;
    }

    HandlerDescription makeHandler(OMElement handlerElement) {
        String name = handlerElement.getAttributeValue(new QName("name"));
        QName qname = handlerElement.resolveQName(name);
        HandlerDescription desc = new HandlerDescription(qname);
        String className = handlerElement.getAttributeValue(new QName("class"));
        desc.setClassName(className);
        return desc;
    }

    public ArrayList getOperationOutPhases() {
        ArrayList oprationOUTPhases = new ArrayList();
        for (int i = 0; i < OUTPhases.size(); i++) {
            String phaseName = (String) OUTPhases.get(i);
            if (PhaseMetadata.PHASE_TRANSPORT_OUT.equals(phaseName)) {
            } else {
                oprationOUTPhases.add(new Phase(phaseName));
            }
        }
        oprationOUTPhases.add(new Phase(PhaseMetadata.PHASE_POLICY_DETERMINATION));
        oprationOUTPhases.add(new Phase(PhaseMetadata.PHASE_MESSAGE_OUT));
        return oprationOUTPhases;
    }

    public ArrayList getOperationInFaultPhases() {
        ArrayList oprationIN_FaultPhases = new ArrayList();
        for (int i = 0; i < IN_FaultPhases.size(); i++) {
            String phaseName = (String) IN_FaultPhases.get(i);
            oprationIN_FaultPhases.add(new Phase(phaseName));
        }
        return oprationIN_FaultPhases;
    }

    public ArrayList getOperationOutFaultPhases() {
        ArrayList oprationOUT_FaultPhases = new ArrayList();
        for (int i = 0; i < OUT_FaultPhases.size(); i++) {
            String phaseName = (String) OUT_FaultPhases.get(i);
            oprationOUT_FaultPhases.add(new Phase(phaseName));
        }
        return oprationOUT_FaultPhases;
    }

    public void setOperationPhases(AxisOperation axisOperation) {
        if (axisOperation != null) {
            axisOperation.setRemainingPhasesInFlow(getOperationInPhases());
            axisOperation.setPhasesOutFlow(getOperationOutPhases());
            axisOperation.setPhasesInFaultFlow(getOperationInFaultPhases());
            axisOperation.setPhasesOutFaultFlow(getOperationOutFaultPhases());
        }
    }

}

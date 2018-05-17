/**
 *  vThing Contact Sensor
 *
 *  Copyright 2018 Todd Long
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
    definition (name: "vThing Contact Sensor", namespace: "tmlong", author: "Todd Long") {
        capability "Actuator"
        capability "Contact Sensor"
        capability "Refresh"
        capability "Sensor"
    }

    simulator {
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "contact", type: "generic", width: 6, height: 4) {
            tileAttribute("device.contact", key: "PRIMARY_CONTROL") {
                attributeState "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#e86d13"
                attributeState "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#00A0DC"
                attributeState "someOpen", label: "some open", icon: "st.contact.contact.closed", backgroundColor: "#79b821"
            }
        }

        standardTile("refresh", "device.contact", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        main "contact"
        details(["contact", "refresh"])
    }
}

def get_SensorState() {
    [ OPEN: "open", CLOSED: "closed", SOME: "someOpen" ]
}

//
// Determine the sensor state.
//
def determineState(sensors) {
    log.debug "determineState() sensors: ${sensors}"

    // determine which sensors are opened
    def sensorsOpen = sensors.count { it.currentContact == _SensorState.OPEN }

    // determine the sensor state
    def sensorState = sensorsOpen == sensors.size() ? _SensorState.OPEN
        : (!sensorsOpen ? _SensorState.CLOSED : _SensorState.SOME)

    log.debug "determineState() sensorsOpen: ${sensorsOpen} sensorState: ${sensorState}"

    sensorState
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
}

// handle commands
def refresh() {
    log.debug "refresh()"

    // send the current state event
    sendEvent(name: "contact", value: determineState(parent.delegates))
}
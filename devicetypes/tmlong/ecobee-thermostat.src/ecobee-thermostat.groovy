/**
 *  Ecobee Thermostat
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
    definition (name: "Ecobee Thermostat", namespace: "tmlong", author: "Todd Long") {
        capability "Actuator"
        capability "Refresh"
        capability "Switch"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    // tile definitions
    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "generic", width: 3, height: 2, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label: 'on', action: "switch.off", icon: "st.alarm.temperature.normal", backgroundColor: "#00A0DC"
                attributeState "off", label: 'off', action: "switch.on", icon: "st.alarm.temperature.normal", backgroundColor: "#ffffff"
            }
        }

        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main "switch"
        details(["switch","refresh"])
    }
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
}

// handle commands
def on() {
    log.debug "on()"
    sendEvent(name: "switch", value: "on")
}

def off() {
    log.debug "off()"
    sendEvent(name: "switch", value: "off")
}

def refresh() {
    log.debug "refresh()"
}

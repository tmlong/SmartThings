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
        capability "Battery"
        capability "Contact Sensor"
        capability "Refresh"
        capability "Sensor"
        capability "Temperature Measurement"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "contact", type: "generic", width: 6, height: 4) {
            tileAttribute("device.contact", key: "PRIMARY_CONTROL") {
                attributeState "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#00A0DC"
                attributeState "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#ffffff"
                attributeState "someOpen", label: "some open", icon: "st.contact.contact.closed", backgroundColor: "#79b821"
            }
        }

        valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
            state "temperature", label: '${currentValue}°',
                    backgroundColors: [
                            [value: 31, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
        }

        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "battery", label: '${currentValue}% battery', unit: ""
        }

        standardTile("refresh", "device.contact", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        main (["contact", "temperature"])
        details(["contact", "temperature", "battery", "refresh"])
    }
}

def get_ContactState() {
    [ OPEN: "open", CLOSED: "closed", SOME: "someOpen" ]
}

//
// Determine the contact state.
//
def determineState(contacts) {
    log.debug "determineState() contacts: ${contacts}"

    // determine which contacts are open
    def contactsOpen = contacts.count { it.currentContact == _ContactState.ON }

    // determine the contact state
    def contactState = contactsOpen == contacts.size() ? _ContactState.ON
        : (!contactsOpen ? _ContactState.OFF : _ContactState.SOME)

    log.debug "determineState() contactState: ${contactState}"

    contactState
}

//
// Check if the device is in the current contact state.
//
def inState(contactState) {
    log.debug "inState() contactState: ${contactState}"

    (contactState == device.currentValue("contact"))
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
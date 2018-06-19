/**
 *  Raspberry Pi LED Switch
 *
 *  Licensed under the GNU v3 (https://www.gnu.org/licenses/gpl-3.0.en.html)
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
preferences {		
	input("ip", "string", title:"IP Address", description: "192.168.1.150", defaultValue: "192.168.1.150" ,required: true, displayDuringSetup: true)		
	input("port", "string", title:"Port", description: "80", defaultValue: "80" , required: true, displayDuringSetup: true)		
}
metadata {
	definition (name: "Raspberry Pi LED Switch (Manual IP)", namespace: "cl0udninja", author: "Janos Elohazi") {
		capability "Polling"
		capability "Refresh"
		capability "Actuator"
		capability "Switch"
        capability "Health Check"

		command "turnOffLed"
        command "turnOnLed"
        command "poll"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles() {
    	multiAttributeTile(name:"toggleLed", type: "lighting", width: 3, height: 3, canChangeIcon: true) {
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
			}
    	}
        standardTile("refresh", "device.refresh", inactiveLabel: false, width: 1, height: 1, decoration: "flat") {
        	state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
        }
        
        main "toggleLed"
        
        details(["toggleLed", "refresh"])
    }
}

def installed() {
	log.debug "installed"
	initialize();
}

def updated() {
	log.debug "updated"
	initialize();
}

def ping() {
	log.debug "ping"
	poll()
}

def initialize() {
	log.debug "initialize"
	sendEvent(name: "checkInterval", value: 60 * 10, data: [protocol: "cloud"], displayed: false)
    refresh()
}

def on() {
	def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)
    
    def uri = "/api/led"
    def headers=[:]
    headers.put("HOST", "${ip}:${port}")
    headers.put("Accept", "application/json")
    headers.put("Content-type", "application/json")
    def body = "{\"pinState\":\"HIGH\"}"
    sendHubCommand(new physicalgraph.device.HubAction(
        method: "POST",
        path: uri,
		headers: headers,
        body: body,
        "${ipHex}:${portHex}"
    ))
    sendEvent(name: "switch", value: "on")
}

def off() {
	def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)
    
    def uri = "/api/led"
    def headers=[:]
    headers.put("HOST", "${ip}:${port}")
    headers.put("Accept", "application/json")
    headers.put("Content-type", "application/json")
    def body = "{\"pinState\":\"LOW\"}"
   	sendHubCommand(new physicalgraph.device.HubAction(
        method: "POST",
        path: uri,
		headers: headers,
        body: body,
        "${ipHex}:${portHex}"
    ))
    sendEvent(name: "switch", value: "off")
}

// parse events into attributes
def parse(description) {
	log.debug "Parse ${description}"
    if (!description.hasProperty("body")) {
    	log.debug "Skipping parse"
        return
    }
    log.debug "Parsing '${description?.body}'"
	def msg = parseLanMessage(description?.body)
    log.debug "Msg ${msg}"
	def json = parseJson(description?.body)
    log.debug "JSON '${json}'"
    
    if (json.containsKey("pinState")) {
    	sendEvent(name: "switch", value: "HIGH".equals(json.pinState) ? "on" : "off")
    }
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    getLedState()
}

def refresh() {
	log.debug "Executing 'refresh'"
    getLedState()
}

private getLedState() {
	def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)

    def uri = "/api/led"
    def headers=[:]
    headers.put("HOST", "${ip}:${port}")
    headers.put("Accept", "application/json")
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: uri,
		headers: headers,
        "${ipHex}:${portHex}",
        [callback: parse]
    )
    log.debug "Getting Pi data ${hubAction}"
    hubAction 
}

private String convertIPtoHex(ipAddress) {
	log.debug "convertIPtoHex ${ipAddress} to hex"
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	log.debug "convertPortToHex ${port} to hex"
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

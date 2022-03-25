import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import com.hp.opr.api.scripting.Event;

import groovy.json.JsonSlurper

class JiraAdapter {


    String userName="jira_user"
    String projectKey="SKOP"
    String password="MDcxOVNLX0JpbGtlbnRfT3BzQnJpZGdl"
    String jiraURL="https://jiraserver.test.local"

    private static Log s_log=LogFactory.getLog("JiraTicketIntegration")

    def getAuthorizationCode(){
      String decodedPass=new String(password.decodeBase64())
      String authorizationCode="Basic "+(userName+":"+decodedPass).bytes.encodeBase64().toString()
      s_log.info("AuthorizationCode alindi")
      return authorizationCode
    }


    def createIssueContent(String event_title="", String event_description = "", List<String> event_components = []) {
      """
          {
              "fields": {
                  "project": { "key": "${projectKey}" },
                  "summary": "PRJ: ${event_title}",
                  "description": "${event_description}",
                  "issuetype": { "name":"Task" },
                  "components":[${event_components.collect{'{"name":"' + it + '"}'}.join(",")}],
                  "priority": { "name": "Critical" }
                }
          }
      """
    }

    def getComponents(String event_cma){
      def componentsList=[]
      if (event_cma.indexOf("PRJ-Microsoft")>-1){
        componentsList.add("Sanallastirma_Alarm_Sorumlusu")
        return componentsList
      }else if (event_cma.indexOf("PRJ-Database")>-1){
        componentsList.add("VeriTabani_KritikAlarm_Sorumlusu")
        componentsList.add("VeriTabani_Alarm_Sorumlusu")
        return componentsList
      }else if (event_cma.indexOf("PRJ-PRJAPP-Uygulama")>-1){
        componentsList.add("PRJAPP_KritikAlarm_Sorumlusu")
        componentsList.add("PRJAPP_Alarm_Sorumlusu")
        return componentsList
      }

      return componentsList
    }

    def getTicketInfo(String postContent) {
        def ticketInfo=[]
        def connection = (jiraURL+"/rest/api/2/issue").toURL().openConnection()
        connection.addRequestProperty("Authorization", getAuthorizationCode())
        connection.addRequestProperty("Content-Type", "application/json")

        connection.setRequestMethod("POST")
        connection.doOutput = true
        connection.outputStream.withWriter{
            it.write(postContent)
            it.flush()
        }
        connection.connect()

        try {

          def jsonSlurper = new JsonSlurper()
          def object = jsonSlurper.parseText(connection.content.text)

          ticketInfo.add(object.key)
          ticketInfo.add("${jiraURL}/browse/"+object.key)
		  return ticketInfo

        } catch (IOException e) {
            try {
                ((HttpURLConnection)connection).errorStream.text
            } catch (Exception ignored) {
              s_log.error("Jira Ticket acilirken hata alindi. HATA:"+e)

                throw e
            }
        }
    }

    def init() {
      s_log.info("Jira Ticket Integration script successfully initialized.")
    }


    def destroy() {
      s_log.info("Jira Ticket Integration script successfully disabled.")
    }

    def process(List<Event> events) {
      try
      {
        events.each {
          event -> modifyEvent(event);

          if(Thread.interrupted()){
            s_log.error("Event Listesi islenirken hata alindi");
            throw new InterruptedException()
          }
        }
      } catch(InterruptedException e) {
        s_log.error("Event listesi islenirken hata alindi; HATA : "+e);
        return
      }
    }

    def modifyEvent (Event event) {

      String event_title=event.getTitle().trim()
      String event_cma=event.getCustomAttribute("cma1").trim()
      String event_description=event.getDescription().trim()
      List<String> compList=getComponents(event_cma)
      String jsonMessage=createIssueContent(event_title,event_description,compList)
      def jiraTicket=getTicketInfo(jsonMessage)

      if (jiraTicket.size()>0){
        event.addCustomAttribute("JiraTicketKey",jiraTicket[0]);
        event.addCustomAttribute("JiraTicketURL",jiraTicket[1]+"/browse/"+jiraTicket[0]);
        s_log.info(" event icin acilan Jira Numarasi: "+jiraTicket[0]);
        s_log.info(" event icin acilan Jira Linki: "+jiraTicket[1]+"/browse/"+jiraTicket[0]);
      }else {
        s_log.error(event_title+ " olayi icin Jira Ticket Acilamadi");
      }

    }

}

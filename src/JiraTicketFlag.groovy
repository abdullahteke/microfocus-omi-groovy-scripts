import java.util.List;
import com.hp.opr.api.scripting.Event;
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class JiraTicketFlag
{
	def groupName="";
		def eventUUID="";
		private static Log s_log = LogFactory.getLog("JiraTicketFlag")
	
	def init()
	{
		s_log.info("Jira Ticket Flag script successfully initialized.")
	}

	def destroy()
	{
	}

	def process(List<Event> events)
	{
		try
		{
			events.each {
				event -> modifyEvent(event); 
				if(Thread.interrupted()){
					s_log.error(" Event Listesi islenirken hata alindi");
					throw new InterruptedException()
				}
					
			}
		}
		catch(InterruptedException e)
		{
			s_log.error("Event listesi islenirken hata alindi; HATA : "+e);	
			return
		}
	}
  
	
	def modifyEvent (Event event){
                              eventUUID=event.getId().toString();
		
		event.addCustomAttribute("JiraTicketFlag","true");
		s_log.info(eventUUID+" eventi için Jira Flag set edildi.");
		
		groupName=event.getAssignedGroupName();
		
		if (groupName!=null && groupName!=""){
		
			switch(groupName) {            
				case "Test1": 
					event.addCustomAttribute("Assignee","ateke");
					break; 
				case "Test2": 
					event.addCustomAttribute("Assignee","ateke2"); 
					break; 
				default: 
					event.addCustomAttribute("Assignee","ateke3");
					break; 
			}
			s_log.info(eventUUID+" eventi için atanmis grup:" + groupName);
			s_log.info(eventUUID+" eventi için atanmis kullanici:" + event.getCustomAttribute("Assignee"));
		}else{
			s_log.warn(eventUUID+" eventi için atanmis grup bulunamadi");
			event.addCustomAttribute("Assignee","ateke"); 
		}
	}
}
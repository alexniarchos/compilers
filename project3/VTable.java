import java.util.LinkedHashMap;

public class VTable{
    public String className;
    public String parentName;
    public LinkedHashMap<String,Integer> dataMembers;
    public LinkedHashMap<String,Integer> functions;

    public VTable(String cn,String pn){
        // initialization
        dataMembers = new LinkedHashMap<String,Integer>();
        functions = new LinkedHashMap<String,Integer>();
    }

}
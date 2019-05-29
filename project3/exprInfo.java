public class exprInfo{
    String value;
    String type;
    String originalType;
    String arglist;

    public exprInfo(String type, String value){
        this.type = type;
        this.value = value;
    }

    public exprInfo(String type, String value, String originalType){
        this.type = type;
        this.value = value;
        this.originalType = originalType;
    }

    public exprInfo(String arglist){
        this.arglist = arglist;
    }

    public exprInfo(){
        
    }
}
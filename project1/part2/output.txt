public class Main {
	public static String findLangType(String langName){
		return (((langName).startsWith("Java")) ? ((("Java").startsWith(langName)) ? "Static" : (((langName).endsWith("script")) ? "Dynamic" : "Unknown")) : (((langName).endsWith("script")) ? "Probably Dynamic" : "Unknown"));
	}
	public static void main(String[] args) {
		System.out.println(findLangType("Java"));
		System.out.println(findLangType("Javascript"));
		System.out.println(findLangType("Typescript"));
	}
}

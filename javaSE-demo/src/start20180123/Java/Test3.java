import java.util.Scanner;
public class Test3{
	public static void main(String[] args){
		Scanner in=new Scanner(System.in);
		System.out.println("������java�ɼ�:");
		int Java=in.nextInt();
		System.out.println("������SQL�ɼ�:");
		int SQL=in.nextInt();
		if(Java>90&&SQL>90){
			System.out.println("����һƪ��״");
		}
	}
}
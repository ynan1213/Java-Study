package Array;

public class TestArray {
	public static void main(String[] args){
		int[] a={5,9,1,7,3,8,6,4,2};
		Array.print(a);
		
		//Array.arr1(a);//ֱ������
		//Array.print(a);
		
		//Array.arr2(a);//ֱ��������Ż��汾
		//Array.print(a);//
		
		//Array.arr3(a);//ð������
		//Array.print(a);//
		
		Array.arr4(a);//��������
		Array.print(a);//
		
		//Array.arr5(a);//��ת����
		//Array.print(a);
	}	
}


class Array{							
	public static void arr1(int[] a){		//ѡ������
		for(int i=0;i<a.length-1;i++){
			for(int j=i+1;j<a.length;j++){
				if(a[i]>a[j]){
					int temp=a[i];
					a[i]=a[j];
					a[j]=temp;
				}
			}
		}
	}
	public static void arr2(int[] a){		//ѡ��������Ż��汾����¼�±꣩
		int k,temp;
		for(int i=0;i<a.length-1;i++){
			k=i;
			for(int j=i+1;j<a.length;j++){
				if(a[k]>a[j]){
					k=j;
				}
			}
			if(i!=k){
				temp=a[i];
				a[i]=a[k];
				a[k]=temp;	
			}
		}
	}
	public static void arr3(int[] a){		//ð������
		//for(int i=0;i<a.length-1;i++){
			//for(int j=0;j<a.length-i-1;j++){
	  for(int i = a.length - 1; i > 0; i--){	//��һ�ֱ����ʽ
		 for(int j=0;j<i;j++){
				if(a[j]>a[j+1]){
					int temp=0;
					temp=a[j];
					a[j]=a[j+1];
					a[j+1]=temp;	
				}
			}
		}
	}
	/*
	 public static void arr4(int[] a){	//ֱ�Ӳ��������
		for(int i=1;i<a.length;i++){
			int j=i-1;
			int temp=a[i];
			for(j=i-1;j>=0&&a[j]>temp;j--){
				a[j+1]=a[j];
			}
			a[j+1]=temp;
		}
	}
	*/
	public static void arr4(int[] a){	//ֱ�Ӳ��������
		for(int i=1;i<a.length;i++){
			int j=i-1;
			int temp=a[i];
			while(j>=0&&a[j]>temp){
				a[j+1]=a[j];
				j--;
			}
			a[j+1]=temp;
		}
	}
	public static void arr5(int[] a){ //��ת����
		for(int i=0;i<a.length/2;i++){
			int temp=a[a.length-i-1];
			a[a.length-i-1]=a[i];
			a[i]=temp;
		}
	}
	
	
	public static void print(int[] a){
		for(int i=0;i<a.length;i++){
			System.out.print(a[i]+" ");
		}
		System.out.println();
	}	
}


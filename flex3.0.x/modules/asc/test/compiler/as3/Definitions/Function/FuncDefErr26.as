package Errors {

 interface _If2Arg
 {
    function fn(argA:Number,argB:String);
 }

 class FuncDefErr26 implements _If2Arg
 {
    public function fn(argA:String) {};
 }
}

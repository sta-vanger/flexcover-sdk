try
{
	print("outer-try");
	throw new Error("blah");
}
catch(e:Error)
{
	print("outer-catch");
	try
	{
		print("inner-try1");
	}
	catch(e:Error)
	{
		print("inner-catch1");
	}
	finally
	{
		print("inner-finally1");
		try
		{
			print("inner-inner-try");
		}
		finally
		{
			print("inner-inner-finally");
		}
	}
	throw e;
}
finally
{
	try
	{
		print("inner-try2");
	}
	finally
	{
		print("inner-finally2");
	}
	print("outer-finally");
}
print("shouldn't show up");
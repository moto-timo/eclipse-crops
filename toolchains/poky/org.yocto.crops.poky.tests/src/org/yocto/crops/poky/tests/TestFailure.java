package org.yocto.crops.poky.tests;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestFailure {

	@Test(expected=AssertionError.class)
	public void testFailure() throws Exception{
		fail();
	}

}

package resonantinduction.core.nei;

import resonant.lib.utility.LanguageUtility;
import resonantinduction.core.ResonantInduction.RecipeType;

public class RICrusherRecipeHandler extends RITemplateRecipeHandler
{

	@Override
	public String getRecipeName()
	{
		return LanguageUtility.getLocal("resonantinduction.machine.crusher");
	}

	@Override
	public RecipeType getMachine()
	{
		return RecipeType.CRUSHER;
	}
}

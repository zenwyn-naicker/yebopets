namespace YeboPets.Api;

public static class MatchLanguage {
    public static MatchResult Apply(MatchResult result, AdopterProfile p, Animal a, string language) {
        if(language != "zu") return result;
        static string Yes(bool value)=>value ? "Yebo" : "Cha";
        static string Activity(string value)=>value switch {
            "Calm"=>"Okuzolile", "Moderate"=>"Okulingene", "Active"=>"Okukhuthele", _=>"Okukhuthele kakhulu"
        };
        var concerns=new List<string>();
        if(a.NeedsYard && !p.HasSecureYard) concerns.Add("Kudingeka igceke elivikelekile.");
        if(p.HasChildren && !a.GoodWithChildren) concerns.Add("Lesi silwane asibhalwanga njengesifanele ukuhlala nezingane.");
        if(p.ExistingPets>0 && !a.GoodWithPets) concerns.Add("Lesi silwane asibhalwanga njengesifanele ukuhlala nezinye izilwane.");
        var names=new[]{"Abasekhaya","Indlela yokuphila","Okuncamelayo","Ulwazi","Ibanga"};
        var reasons=new[]{
            concerns.Count==0 ? "Izidingo zasekhaya ziyafana nezimpendulo zakho." : string.Join(" ",concerns),
            $"Ukuzivocavoca kwakho: {Activity(p.Activity)}; kwesilwane: {Activity(a.Activity)}.",
            $"Uhlobo luyafana: {Yes(p.PreferredSpecies=="Any" || p.PreferredSpecies==a.Species)}; usayizi uyafana: {Yes(p.PreferredSize=="Any" || p.PreferredSize==a.Size)}; iminyaka iyafana: {Yes(p.PreferredAge=="Any" || p.PreferredAge==AnimalRules.AgeGroup(a.AgeMonths))}.",
            $"Ulwazi lwakho: {p.Experience}/2; izinga elidingekayo: {a.ExperienceRequired}/2.",
            $"Ibanga elilinganiselwe phakathi kwezikhungo zamadolobha: {result.DistanceKm:F1} km; umkhawulo wakho: {p.MaxDistanceKm} km."
        };
        return result with {
            Factors=result.Factors.Select((f,i)=>f with {Name=names[i],Reason=reasons[i]}).ToList(),
            Advice="Lesi silinganiso siyisiqondiso, asiyona imvume yokwamukela. Ukukhathazeka ngekhaya kudinga ukubuyekezwa kwendawo yokunakekela."
        };
    }
}
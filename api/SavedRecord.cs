using MongoDB.Bson.Serialization.Attributes;

namespace YeboPets.Api;

public sealed class SavedRecord
{
	[BsonId]
	public string Id { get; set; } = "";

	public Dictionary<string, string> Operations { get; set; } = new();

	public List<string> AnimalIds { get; set; } = [];
}
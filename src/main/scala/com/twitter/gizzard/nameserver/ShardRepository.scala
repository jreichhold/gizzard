package com.twitter.gizzard.nameserver

import scala.collection.mutable
import shards.{ShardInfo, ShardFactory}


class ShardRepository[S <: shards.Shard] {
  private val shardFactories = mutable.Map.empty[String, ShardFactory[S]]

  def +=(item: (String, ShardFactory[S])) {
    shardFactories += item
  }

  def find(shardInfo: ShardInfo, weight: Int, children: Seq[S]) = {
    factory(shardInfo.className).instantiate(shardInfo, weight, children)
  }

  def create(shardInfo: ShardInfo) {
    factory(shardInfo.className).materialize(shardInfo)
  }
  
  def factory(className: String) = {
    shardFactories.get(className).getOrElse {
      val classes = shardFactories.keySet
      val message = "No such class: " + className + "\nValid classes:\n" + classes
      throw new NoSuchElementException(message)
    }
  }

  override def toString() = {
    "ShardRepository(" + shardFactories.toString + ")"
  }
}

/**
 * A ShardRepository that is pre-seeded with read-only, write-only, replicating, and blocked
 * shard types.
 */
class BasicShardRepository[S <: shards.Shard](constructor: shards.ReadWriteShard[S] => S, future: Future)
      extends ShardRepository[S] {

  setupPackage("com.twitter.gizzard.shards")

  def setupPackage(packageName: String) {
    this += (packageName + ".ReadOnlyShard"    -> new shards.ReadOnlyShardFactory(constructor))
    this += (packageName + ".BlockedShard"     -> new shards.BlockedShardFactory(constructor))
    this += (packageName + ".WriteOnlyShard"   -> new shards.WriteOnlyShardFactory(constructor))
    this += (packageName + ".ReplicatingShard" ->
             new shards.ReplicatingShardFactory(constructor, future))
  }
}

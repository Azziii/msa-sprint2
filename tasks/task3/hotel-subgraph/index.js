import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';
import { buildSubgraphSchema } from '@apollo/subgraph';
import gql from 'graphql-tag';
import DataLoader from 'dataloader';

const hotels = [
  { id: 'h1', name: 'Hilton', city: 'Seoul', stars: 5 },
  { id: 'h2', name: 'Marriott', city: 'Seoul', stars: 4 },
  { id: 'h3', name: 'Lotte', city: 'Seoul', stars: 4 },
];

const createHotelLoader = () =>
  new DataLoader(async (ids) => {
    console.log('BATCH LOAD:', ids);

    return ids.map(id => hotels.find(h => h.id === id));
  });

const hotelLoader = createHotelLoader();

const typeDefs = gql`
  type Hotel @key(fields: "id") {
    id: ID!
    name: String
    city: String
    stars: Int
  }

  type Query {
    hotelsByIds(ids: [ID!]!): [Hotel]!
  }
`;

const resolvers = {
  Hotel: {
    __resolveReference: (hotel) => {
      if (!hotel || !hotel.id) {
        return null;
      }

      return hotelLoader.load(hotel.id);
    },
  },

  Query: {
    hotelsByIds: (_, { ids }) => {
      return ids.map(id => hotels.find(h => h.id === id));
    },
  },
};

const server = new ApolloServer({
  schema: buildSubgraphSchema([{ typeDefs, resolvers }]),
});

startStandaloneServer(server, {
  listen: { port: 4002 },
}).then(() => {
  console.log('✅ Hotel subgraph ready at http://localhost:4002/');
});
import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';
import { buildSubgraphSchema } from '@apollo/subgraph';
import gql from 'graphql-tag';

const typeDefs = gql`
  enum BookingStatus {
    CONFIRMED
    CANCELLED
    PENDING
  }

  type Booking @key(fields: "id") {
    id: ID!
    userId: ID!
    hotelId: ID!
    promoCode: String
    discountPercent: Float  # Базовое значение из БД бронирований
    checkIn: String!
    checkOut: String!
    status: BookingStatus!
    hotel: Hotel
  }
  
  extend type Hotel @key(fields: "id") {
    id: ID! @external
  }

  type Query {
    bookingsByUser(userId: ID!): [Booking!]!
    booking(id: ID!): Booking
  }

`;

const resolvers = {
  Query: {
    bookingsByUser: async (_, { userId }, { req }) => {
      console.log('HEADERS:', req.headers);
      const requesterId = req.headers['userid'];

      // ACL
      if (!requesterId || requesterId !== userId) {
        return [];
      }

      return bookings.filter(b => b.userId === userId);
    },

    booking: (_, { id }) => {
      return bookings.find(b => b.id === id);
    }
  },
  Booking: {
	  hotel: (booking) => ({
      __typename: "Hotel",
      id: booking.hotelId
    })
  },
};

const server = new ApolloServer({
  schema: buildSubgraphSchema([{ typeDefs, resolvers }]),
});

startStandaloneServer(server, {
  listen: { port: 4001 },
  context: async ({ req }) => ({ req }),
}).then(() => {
  console.log('✅ Booking subgraph ready at http://localhost:4001/');
});

const bookings = [
  {
    id: 'b1',
    userId: 'user1',
    hotelId: 'h1',
    promoCode: 'SUMMER',
    discountPercent: 10,
    checkIn: '2025-01-01',
    checkOut: '2025-01-05',
    status: 'CONFIRMED'
  }
];
